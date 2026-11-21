package com.xgdt.dx11;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Guards against a MixinExtras runtime bug that only shows up on a real client.
 *
 * <p>Fabric Loader 0.19.x bundles MixinExtras 0.5.4.  Its
 * {@code FactoryRedirectWrapperMixinTransformer} reads the {@code at} member of
 * every {@code @Redirect} as a single {@code AnnotationNode}, but Mixin stores
 * it as a one-element list, so any mixin that uses {@code @Redirect} throws a
 * {@code ClassCastException} while the target class is transformed.  The
 * crash happens at class load time, not at startup, so it is invisible to the
 * unit tests unless we check for it here.</p>
 *
 * <p>Use {@code @WrapOperation} instead.  This test fails if a mixin in this
 * jar reintroduces {@code @Redirect}.</p>
 */
class MixinCompatibilityTest {
    private static final String REDIRECT_DESCRIPTOR =
        "Lorg/spongepowered/asm/mixin/injection/Redirect;";

    @Test
    void perfMixinsDoNotUseRedirect() throws IOException, URISyntaxException {
        Path mainClasses = mainClassDirectory();
        Path mixinClasses = mainClasses.resolve("com/xgdt/dx11/mixin");
        assertTrue(Files.isDirectory(mixinClasses), "mixin output missing: " + mixinClasses);

        List<String> offenders = new ArrayList<>();
        try (Stream<Path> files = Files.walk(mixinClasses)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".class")).toList()) {
                String constantPool =
                    new String(Files.readAllBytes(file), StandardCharsets.ISO_8859_1);
                if (constantPool.contains(REDIRECT_DESCRIPTOR)) {
                    offenders.add(mixinClasses.relativize(file).toString());
                }
            }
        }

        assertEquals(
            List.of(),
            offenders,
            "MixinExtras 0.5.4 (bundled with Fabric Loader 0.19.x) throws ClassCastException "
                + "while scanning any @Redirect; use @WrapOperation instead. Offending classes: "
                + offenders);
    }

    private static Path mainClassDirectory() throws URISyntaxException {
        Path testClasses = Path.of(
            MixinCompatibilityTest.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        return testClasses.getParent().resolve("main");
    }
}
