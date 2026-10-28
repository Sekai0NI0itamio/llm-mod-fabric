package com.llama4j;

public class QwenChatFormat {
    protected final QwenTokenizer tokenizer;
    private final int imStart;
    private final int imEnd;
    private final int thinkOpen;
    private final int thinkClose;

    public QwenChatFormat(QwenTokenizer tokenizer) {
        this.tokenizer = tokenizer;
        Map<String, Integer> specials = tokenizer.specialTokens();
        this.imStart = specials.get("<|im_start|>");
        this.imEnd = specials.get("<|im_end|>");
        this.thinkOpen = specials.getOrDefault("<think>", -1);
        this.thinkClose = specials.getOrDefault("</think>", -1);
    }

    public Set<Integer> getStopTokens() {
        Set<Integer> tokens = new HashSet<>();
        tokens.add(tokenizer.getEosToken());
        tokens.add(imEnd);
        return tokens;
    }

    public List<Integer> encodeMessage(String role, String content) {
        List<Integer> tokens = new ArrayList<>();
        tokens.add(imStart);
        tokens.addAll(tokenizer.encodeAsList(role + "\n" + content));
        tokens.add(imEnd);
        tokens.addAll(tokenizer.encodeAsList("\n"));
        return tokens;
    }

    public List<Integer> encodeHeader(String role, boolean think) {
        List<Integer> tokens = new ArrayList<>();
        tokens.add(imStart);
        tokens.addAll(tokenizer.encodeAsList(role + "\n"));
        if ("assistant".equals(role) && !think && thinkOpen >= 0 && thinkClose >= 0) {
            // Add empty think block to disable thinking mode
            tokens.add(thinkOpen);
            tokens.addAll(tokenizer.encodeAsList("\n\n"));
            tokens.add(thinkClose);
            tokens.addAll(tokenizer.encodeAsList("\n\n"));
        }
        return tokens;
    }
}
