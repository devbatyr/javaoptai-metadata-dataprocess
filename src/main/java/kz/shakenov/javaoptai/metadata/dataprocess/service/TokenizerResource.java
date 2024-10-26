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
import java.util.concurrent.atomic.AtomicInteger;

@ApplicationScoped
public class TokenizerResource {

    @Inject
    RepositoryService repositoryService;
    @Inject
    ObjectMapper objectMapper;

    public void tokenize(String repositoriesPath) {
        List<Path> repositoriesPathsList = repositoryService.findRepositories(repositoriesPath);
        File outputDir = new File("tokens");
        if (!outputDir.exists()) {
            if (!outputDir.mkdirs()) {
                System.err.println("Failed to create directory: " + outputDir.getAbsolutePath());
            }
        }

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failedCount = new AtomicInteger(0);

        for (Path path : repositoriesPathsList) {
            List<Path> javaFiles = repositoryService.findJavaFilesInRepository(path);

            for (Path javaFile : javaFiles) {
                ObjectNode tokens = tokenizeJavaFile(javaFile.toFile());
                if (tokens != null) {
                    saveTokensToFile(outputDir.getPath(), javaFile.getFileName().toString(), tokens);
                    successCount.incrementAndGet();
                } else {
                    failedCount.incrementAndGet();
                }
                System.out.printf("\r%d files tokenized | %d files failed",
                        successCount.get(), failedCount.get());
            }
        }
    }

    private ObjectNode tokenizeJavaFile(File javaFile) {
        try {
            CompilationUnit compilationUnit = StaticJavaParser.parse(javaFile);
            compilationUnit.getAllContainedComments().forEach(Comment::remove);

            ObjectNode result = objectMapper.createObjectNode();
            compilationUnit.findAll(ClassOrInterfaceDeclaration.class).forEach(clazz -> {
                ObjectNode classNode = result.putObject(clazz.getNameAsString());

                clazz.findAll(MethodDeclaration.class)
                        .forEach(method -> classNode.put(method.getNameAsString(), tokenizeMethod(method)));
            });

            return result;
        } catch (Exception exception) {
            return null;
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
