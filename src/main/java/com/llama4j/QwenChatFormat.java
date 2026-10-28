package com.llama4j;

import java.util.*;

public class QwenChatFormat {
    protected final QwenTokenizer tokenizer;
    private final int imStart;
    private final int imEnd;
    private final int thinkOpen;
    private final int thinkClose;

    public QwenChatFormat(QwenTokenizer tokenizer) {
        this.tokenizer = tokenizer;
        Map<String, Integer> specials = tokenizer.specialTokens();
        // Debug: print special tokens containing "im"
        for (Map.Entry<String, Integer> e : specials.entrySet()) {
            if (e.getKey().contains("im_")) {
                System.err.println("[LLMMod] special token: '" + e.getKey() + "' = " + e.getValue());
            }
        }
        Integer is = specials.get(" “");
        if (is == null) {
            // try variants
            for (Map.Entry<String, Integer> e : specials.entrySet()) {
                if (e.getKey().contains("im_start")) { is = e.getValue(); break; }
            }
        }
        Integer ie = specials.get(" “");
        if (ie == null) {
            for (Map.Entry<String, Integer> e : specials.entrySet()) {
                if (e.getKey().contains("im_end")) { ie = e.getValue(); break; }
            }
        }
        this.imStart = (is == null) ? -1 : is;
        this.imEnd = (ie == null) ? -1 : ie;
        this.thinkOpen = specials.getOrDefault("<think>", -1);
        this.thinkClose = specials.getOrDefault("</think>", -1);
        if (imStart < 0 || imEnd < 0) {
            throw new IllegalStateException("Could not find im_start/im_end special tokens. imStart=" + imStart + " imEnd=" + imEnd);
        }
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
            tokens.add(thinkOpen);
            tokens.addAll(tokenizer.encodeAsList("\n\n"));
            tokens.add(thinkClose);
            tokens.addAll(tokenizer.encodeAsList("\n\n"));
        }
        return tokens;
    }
}
