package net.jbock.context;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.ParameterSpec;
import net.jbock.util.ErrTokenType;
import net.jbock.util.ExToken;

import javax.inject.Inject;
import java.util.List;

public class ContextUtil {

    @Inject
    ContextUtil() {
    }

    CodeBlock joinByNewline(List<CodeBlock> code) {
        boolean indent = false;
        CodeBlock.Builder result = CodeBlock.builder();
        for (int i = 0; i < code.size(); i++) {
            if (i == 0) {
                result.add(code.get(i));
            } else if (i == 1) {
                result.add("\n").indent().add(code.get(i));
                indent = true;
            } else {
                result.add("\n").add(code.get(i));
            }
        }
        if (indent) {
            result.unindent();
        }
        return result.build();
    }

    CodeBlock joinByComma(List<CodeBlock> code) {
        CodeBlock.Builder args = CodeBlock.builder();
        for (int i = 0; i < code.size(); i++) {
            if (i != 0) {
                args.add(",$W");
            }
            args.add(code.get(i));
        }
        return args.build();
    }

    CodeBlock throwRepetitionErrorStatement(ParameterSpec token) {
        return CodeBlock.of("throw new $T($T.$L, $N)", ExToken.class, ErrTokenType.class,
                ErrTokenType.OPTION_REPETITION, token);
    }
}