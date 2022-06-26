package gh.marad.chi.truffle.compilation;

import gh.marad.chi.core.Message;

import java.util.Collections;
import java.util.List;

public class CompilationFailed extends RuntimeException {
    private final List<Message> messages;
    public CompilationFailed(List<Message> messages) {
        super("Compilation failed");
        this.messages = Collections.unmodifiableList(messages);
    }

    public List<Message> getMessages() {
        return messages;
    }
}
