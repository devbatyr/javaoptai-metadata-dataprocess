package kz.shakenov.javaoptai.metadata.dataprocess.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.comments.Comment;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@ApplicationScoped
public class TokenizerResource {

    @Inject
    RepositoryService repositoryService;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public void tokenize(String repositoriesPath) {
        List<Path> repositoriesPathsList = repositoryService.findRepositories(repositoriesPath);
        File outputDir = new File("tokens");
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            System.err.println("Failed to create directory: " + outputDir.getAbsolutePath());
        }

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failedCount = new AtomicInteger(0);

        repositoriesPathsList.parallelStream().forEach(path -> {
            List<Path> javaFiles = repositoryService.findJavaFilesInRepository(path);

            javaFiles.forEach(javaFile -> {
                Optional<ObjectNode> tokens = tokenizeJavaFile(javaFile.toFile());
                tokens.ifPresentOrElse(
                        token -> {
                            saveTokensToFile(outputDir.getPath(), javaFile.getFileName().toString(), token);
                            successCount.incrementAndGet();
                        },
                        failedCount::incrementAndGet
                );

                System.out.printf("\r%d files tokenized successfully | %d files failed to tokenize",
                        successCount.get(), failedCount.get());
            });
        });

        System.out.println("\nTokenization finished");
    }

    private Optional<ObjectNode> tokenizeJavaFile(File javaFile) {
        try {
            CompilationUnit compilationUnit = StaticJavaParser.parse(javaFile);
            compilationUnit.getAllContainedComments().forEach(Comment::remove);

            ObjectNode result = objectMapper.createObjectNode();
            compilationUnit.findAll(ClassOrInterfaceDeclaration.class)
                    .forEach(clazz -> {
                        List<MethodDeclaration> methods = clazz.findAll(MethodDeclaration.class);
                        if (!methods.isEmpty()) {
                            ObjectNode classNode = result.putObject(clazz.getNameAsString());
                            methods.forEach(method ->
                                    classNode.put(method.getNameAsString(), tokenizeMethod(method))
                            );
                        }
                    });

            return Optional.of(result);
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private String tokenizeMethod(MethodDeclaration method) {
        return method.toString()
                .replaceAll("\\s+", " ")
                .trim();
    }

    private void saveTokensToFile(String outputDir, String fileName, ObjectNode tokens) {
        File file = new File(outputDir + "/" + fileName.replace(".java", ".json"));
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, tokens);
        } catch (IOException exception) {
            System.err.println("\nFailed to create token file: " + exception.getMessage());
        }
    }
}
