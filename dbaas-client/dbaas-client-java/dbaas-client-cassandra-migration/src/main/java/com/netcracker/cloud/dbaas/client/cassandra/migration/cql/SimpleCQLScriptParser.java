package org.qubership.cloud.dbaas.client.cassandra.migration.cql;

import java.util.ArrayList;
import java.util.List;

public class SimpleCQLScriptParser {
    private enum SymbolState {
        DEFAULT,
        SINGLE_LINE_COMMENT,
        MULTI_LINE_COMMENT,
        QUOTE_STRING,
        SQUOTE_STRING
    }

    private final String scriptText;
    private SymbolState state;
    private int position;

    public SimpleCQLScriptParser(String scriptText) {
        this.state = SymbolState.DEFAULT;
        this.scriptText = scriptText;
        this.position = 0;
    }

    public List<String> parseToCqlQueries() {
        List<String> queries = new ArrayList<>();
        StringBuilder nestedQueries = new StringBuilder();

        char c;
        while ((c = nextSymbol()) != 0) {
            switch (state) {
                case DEFAULT -> {
                    if (c == '/' && currentSymbol() == '/') {
                        state = SymbolState.SINGLE_LINE_COMMENT;
                        incrementPosition();
                    } else if (c == '-' && currentSymbol() == '-') {
                        state = SymbolState.SINGLE_LINE_COMMENT;
                        incrementPosition();
                    } else if (c == '/' && currentSymbol() == '*') {
                        state = SymbolState.MULTI_LINE_COMMENT;
                        incrementPosition();
                    } else if (c == '\n') {
                        nestedQueries.append(' ');
                    } else {
                        nestedQueries.append(c);
                        if (c == '\"') {
                            state = SymbolState.QUOTE_STRING;
                        } else if (c == '\'') {
                            state = SymbolState.SQUOTE_STRING;
                        } else if (c == ';') {
                            queries.add(nestedQueries.toString().trim());
                            nestedQueries.setLength(0);
                        }
                    }
                }
                case SINGLE_LINE_COMMENT -> {
                    if (c == '\n') {
                        state = SymbolState.DEFAULT;
                    }
                }
                case MULTI_LINE_COMMENT -> {
                    if (c == '*' && currentSymbol() == '/') {
                        state = SymbolState.DEFAULT;
                        incrementPosition();
                    }
                }
                case QUOTE_STRING -> {
                    nestedQueries.append(c);
                    if (c == '"') {
                        if (currentSymbol() == '"') {
                            nestedQueries.append(nextSymbol());
                        } else {
                            state = SymbolState.DEFAULT;
                        }
                    }
                }
                case SQUOTE_STRING -> {
                    nestedQueries.append(c);
                    if (c == '\'') {
                        if (currentSymbol() == '\'') {
                            nestedQueries.append(nextSymbol());
                        } else {
                            state = SymbolState.DEFAULT;
                        }
                    }
                }
            }
        }
        String tmp = nestedQueries.toString().trim();
        if (tmp.length() > 0) {
            queries.add(tmp);
        }

        return queries;
    }

    private char nextSymbol() {
        if (position < scriptText.length()) {
            return scriptText.charAt(position++);
        } else {
            return 0;
        }
    }

    private char currentSymbol() {
        if (position < scriptText.length()) {
            return scriptText.charAt(position);
        } else {
            return 0;
        }
    }

    private void incrementPosition() {
        position++;
    }
}
