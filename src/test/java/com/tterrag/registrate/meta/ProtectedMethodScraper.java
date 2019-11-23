package com.tterrag.registrate.meta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.Value;

/**
 * Scrapes all protected methods from pasted source, and emits them as public super-calling stubs. Used to create the bouncer classes such as BuilderModelProvider.
 */
public class ProtectedMethodScraper {

    @Value
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Header {

        String className;
        boolean isStatic;
        String type;
        String name;
        String[] paramTypes;
        String[] paramNames;
        
        public Header applyTypeReplacement(Pair<String, String> repl) {
            String[] newParamTypes = Arrays.copyOf(paramTypes, paramTypes.length);
            for (int i = 0; i < newParamTypes.length; i++) {
                if (newParamTypes[i].equals(repl.getKey())) {
                    newParamTypes[i] = repl.getValue();
                }
            }
            String type = this.type.equals(repl.getKey()) ? repl.getValue() : this.type;
            return new Header(this.className, this.isStatic, type, this.name, newParamTypes, this.paramNames);
        }

        public String printStubMethod() {
            StringBuilder base = new StringBuilder();
            if (!isStatic) {
                base.append("@Override\n");
            }
            base.append("public ").append(isStatic ? "static " : "").append(type).append(" ").append(name).append("(");
            for (int i = 0; i < paramTypes.length; i++) {
                if (i > 0) {
                    base.append(", ");
                }
                base.append(paramTypes[i]).append(" ").append(paramNames[i]);
            }
            base.append(") { ").append(type.equals("void") ? "" : "return ");
            if (isStatic) {
                base.append(className).append(".");
            } else {
                base.append("super.");
            }
            base.append(name).append("(").append(Arrays.stream(paramNames).collect(Collectors.joining(", "))).append("); }");
            return base.toString();
        }

        private static final Pattern PATTERN = Pattern.compile("^\\s+protected\\s(static)?\\s?(\\S+)\\s(\\S+)\\((.+)\\)\\s\\{$");

        public static Optional<Header> parse(String className, String code) {
            Matcher m = PATTERN.matcher(code);
            if (m.matches()) {
                boolean isStatic = m.group(1) != null;
                String type = m.group(2);
                String name = m.group(3);
                String params = m.group(4);
                String[] paramList = params.split(",");
                String[] paramTypes = new String[paramList.length];
                String[] paramNames = new String[paramList.length];
                for (int i = 0; i < paramList.length; i++) {
                    String param = paramList[i].trim();
                    String[] typeAndName = param.split(" ");
                    if (typeAndName.length == 2) {
                        paramTypes[i] = typeAndName[0];
                        paramNames[i] = typeAndName[1];
                    } else {
                        return Optional.empty();
                    }
                }
                return Optional.of(new Header(className, isStatic, type, name, paramTypes, paramNames));
            }
            return Optional.empty();
        }
    }

    public static List<Header> scrapeInput() {
        @SuppressWarnings("resource")
        Scanner scanner = new Scanner(System.in);
        System.out.println("Paste class source:");
        String line = scanner.nextLine();
        List<Header> headers = new ArrayList<>();
        String className = null;
        do {
            if (className != null) {
                Header.parse(className, line).ifPresent(headers::add);
            } else if (line.startsWith("public class ")) {
                className = line.split("\\s+")[2];
            }
            line = scanner.nextLine();
        } while (!"done".equals(line));
        return headers;
    }
}
