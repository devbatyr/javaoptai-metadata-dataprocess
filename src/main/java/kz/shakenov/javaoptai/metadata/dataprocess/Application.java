package kz.shakenov.javaoptai.metadata.dataprocess;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import jakarta.inject.Inject;
import kz.shakenov.javaoptai.metadata.dataprocess.service.TokenizerResource;

import java.util.Scanner;

@QuarkusMain
public class Application implements QuarkusApplication {

    @Inject
    TokenizerResource tokenizerResource;

    @Override
    public int run(String... args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Enter path to repositories folder:");
        String path = scanner.nextLine();

        tokenizerResource.tokenize(path);

        return 0;
    }

    public static void main(String... args) {
        Quarkus.run(Application.class, args);
    }
}
