package com.tterrag.registrate.meta;

import java.io.IOException;
import java.nio.file.Paths;

public class UpdateRecipeProvider {
    
    public static void main(String[] args) throws IOException {
        new MethodGenerator()
            .exclude("registerRecipes")
            .generate(Paths.get("src", "main", "java", "com", "tterrag", "registrate", "providers", "RegistrateRecipeProvider.java"));
    }
}
