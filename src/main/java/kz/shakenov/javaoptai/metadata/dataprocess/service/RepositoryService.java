package kz.shakenov.javaoptai.metadata.dataprocess.service;

import jakarta.enterprise.context.ApplicationScoped;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ApplicationScoped
public class RepositoryService {

    public List<Path> findRepositories(String rootDir) {
        try (Stream<Path> paths = Files.walk(Path.of(rootDir), 1)) {
            return paths
                    .filter(Files::isDirectory)
                    .filter(path -> !path.equals(Path.of(rootDir)))
                    .toList();
        } catch (IOException exception) {
            System.err.println("Failed to get repositories: " + exception.getMessage());
            return Collections.emptyList();
        }
    }

    public List<Path> findJavaFilesInRepository(Path repositoryPath) {
        try (Stream<Path> paths = Files.walk(repositoryPath)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .collect(Collectors.toList());
        } catch (IOException exception) {
            System.err.println("Failed to find Java files: " + exception.getMessage());
            return Collections.emptyList();
        }
    }

}
