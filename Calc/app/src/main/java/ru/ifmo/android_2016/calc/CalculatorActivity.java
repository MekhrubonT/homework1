package ru.ifmo.android_2016.calc;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public final class CalculatorActivity extends Activity implements View.OnClickListener {

    private static final int MAXEXPRESSIONLENGTH = 60;
    private static final int TEXTSIZE = 32;
    private final int BINARYOPERATIONSPRIORITIES = 2;
    private final int RESULTSCALE = 100;

    // ClickTools
    private final RuntimeException wrongFormat;
    Token lastToken = Token.EMPTY;
    boolean comaFlag;
    private int binOpId[] = new int[]{R.id.add, R.id.sub, R.id.mul, R.id.div};
    private char binOpLex[] = new char[]{'+', '-', '*', '/'};
    private int numbersIds[] = new int[]{R.id.d0, R.id.d1, R.id.d2, R.id.d3, R.id.d4, R.id.d5, R.id.d6,
            R.id.d7, R.id.d8, R.id.d9};
    private char numbersLex[] = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};
    // Parser Pointer
    private int pointer;
    private int bracketBalance;
    private StringBuilder lexem;

    private TextView result;
    private String KEY_LEXEM = "lexem";
    private String KEY_TEXTVIEW = "textview";

    {
        wrongFormat = new RuntimeException("Неправильный формат.");
        lexem = new StringBuilder();
    }

    void onClickListener(int[] ids) {
        for (int a : ids) {
            findViewById(a).setOnClickListener(this);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calculator);
        onClickListener(numbersIds);
        onClickListener(binOpId);
        onClickListener(new int[]{R.id.clear, R.id.coma, R.id.eqv, R.id.sign, R.id.parenthessis, R.id.del});


        result = (TextView) findViewById(R.id.result);
        result.setTextColor(Color.BLUE);
        result.setTextSize(TEXTSIZE);
        if (savedInstanceState != null) {
            lexem = (StringBuilder) savedInstanceState.get(KEY_LEXEM);
            result.setText((String) savedInstanceState.get(KEY_TEXTVIEW));
            lastToken.update(lexem);
            if (lexem.length() > 0) {
                int i = lexem.length() - 1;
                while (i >= 0 && (Character.isDigit(lexem.charAt(i)) || lexem.charAt(i) == '.')) {
                    if (lexem.charAt(i) == '.') {
                        comaFlag = true;
                    }
                    i--;
                }
            }
        }
    }

    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putCharSequence(KEY_LEXEM, lexem);
        outState.putCharSequence(KEY_TEXTVIEW, result.getText());
    }


    @Override
    public void onClick(View v) {
        int vId = v.getId();
        String parseResult = "";

        switch (vId) {
            case R.id.clear:
                lexem = new StringBuilder();
                bracketBalance = 0;
                break;
            case R.id.del:
                if (lexem.length() > 0) {
                    int pos = lexem.length() - 1;
                    if (lexem.charAt(pos) == ')') {
                        bracketBalance++;
                    } else if (lexem.charAt(pos) == '(') {
                        bracketBalance--;
                    }
                    lexem.deleteCharAt(pos);
                }
                break;
            case R.id.coma:
                if (!comaFlag) {
                    comaFlag = true;
                    if (!lastToken.equals(Token.DIGIT)) {
                        lexem.append('0');
                    }
                    lexem.append('.');
                }
                break;
            case R.id.sign:
                if (!lastToken.equals(Token.SIGN)) {
                    if (lastToken != Token.EMPTY && lastToken != Token.BINARYOPERATION)
                        lexem.append('*');
                    lexem.append("(-");
                    bracketBalance++;
                } else {
                    lexem.delete(lexem.length() - 2, lexem.length());
                    bracketBalance--;
                }
                break;
            case R.id.parenthessis:
                if ((lastToken.equals(Token.DIGIT) || lastToken.equals(Token.CLBRACKET)) && bracketBalance > 0) {
                    lexem.append(')');
                    bracketBalance--;
                } else {
                    if (bracketBalance == 0 && lastToken != Token.EMPTY && lastToken != Token.BINARYOPERATION)
                        lexem.append('*');
                    lexem.append('(');
                    bracketBalance++;
                }
                break;
            case R.id.eqv:
                while (bracketBalance > 0) {
                    lexem.append(')');
                    bracketBalance--;
                }
                parseResult = parse();
                break;
            default:
                for (int i = 0; i < numbersIds.length; i++) {
                    if (numbersIds[i] == vId) {
                        lexem.append(numbersLex[i]);
                    }
                }
                for (int i = 0; i < binOpId.length; i++) {
                    if (binOpId[i] == vId) {
                        if (lastToken.equals(Token.BINARYOPERATION)) {
                            lexem.setCharAt(lexem.length() - 1, binOpLex[i]);
                        }
                        if (lastToken.equals(Token.DIGIT) || lastToken.equals(Token.CLBRACKET)) {
                            lexem.append(binOpLex[i]);
                        }
                    }
                }
        }


        if (lexem.length() > MAXEXPRESSIONLENGTH) {
            lexem.delete(MAXEXPRESSIONLENGTH, lexem.length());
            Toast.makeText(this, "Максильманое допустимое число символов (" + Integer.toString(MAXEXPRESSIONLENGTH) + ")", Toast.LENGTH_SHORT).show();
        } else {
            lastToken = lastToken.update(lexem);
            result.setText(lexem);

            if (parseResult != null && parseResult.length() != 0) {
                result.append("\n=" + parseResult);
            }
        }
    }


    private java.math.BigDecimal apply(java.math.BigDecimal lf, java.math.BigDecimal rg, char op) {

        switch (op) {
            case '+':
                return lf.add(rg);
            case '-':
                return lf.subtract(rg);
            case '*':
                return lf.multiply(rg);
            case '/':
                if (rg.equals(java.math.BigDecimal.ZERO))
                    throw new RuntimeException("Деление на ноль.");
                return lf.divide(rg, RESULTSCALE, java.math.BigDecimal.ROUND_HALF_UP);
            default:
                throw new RuntimeException("Неопознонная операция.");
        }
    }

    private void skipWhitespaces() {
        while (pointer < lexem.length() && Character.isWhitespace(lexem.charAt(pointer)))
            ++pointer;
    }

    String parse() {
        try {
            pointer = 0;
            System.out.println("new pointer = 0");
            java.math.BigDecimal res = binaryOperations(0);
            if (pointer != lexem.length())
                throw new RuntimeException("Неправильный формат");
            String ans = res.setScale(RESULTSCALE, java.math.BigDecimal.ROUND_HALF_UP).stripTrailingZeros().toString();
            if (ans.length() > RESULTSCALE / 2) {
                ans = String.valueOf(Double.parseDouble(ans));
            }
            return ans;
        } catch (IndexOutOfBoundsException e) {
            return "Неправильный формат";
        } catch (RuntimeException d) {
            if (d.getMessage().isEmpty())
                return "Неправильный формат";
            return d.getMessage();
        } catch (Exception e) {
            return "Unchecked exception";
        }
    }


    java.math.BigDecimal binaryOperations(int level) {
        if (level == BINARYOPERATIONSPRIORITIES) {
            return unaryBracketsNumber();
        }
        java.math.BigDecimal res = binaryOperations(level + 1);
        while (pointer < lexem.length()) {
            skipWhitespaces();
            char cur = lexem.charAt(pointer);
            if (cur != binOpLex[level * 2] && cur != binOpLex[level * 2 + 1])
                break;
            pointer++;
            java.math.BigDecimal next = binaryOperations(level + 1);
            res = apply(res, next, cur);
        }
        return res;
    }

    java.math.BigDecimal unaryBracketsNumber() {
        skipWhitespaces();
        switch (lexem.charAt(pointer)) {
            case '(':
                pointer++;
                java.math.BigDecimal res = binaryOperations(0);
                if (lexem.charAt(pointer) != ')') {
                    throw wrongFormat;
                }
                pointer++;
                return res;
            case '-':
                pointer++;
                return binaryOperations(0).negate();
            default:
                if (!Character.isDigit(lexem.charAt(pointer)))
                    throw new RuntimeException();
                int startPointer = pointer;
                while (pointer < lexem.length() && (Character.isDigit(lexem.charAt(pointer)) || lexem.charAt(pointer) == '.')) {
                    pointer++;
                }
                return new java.math.BigDecimal(lexem.substring(startPointer, pointer));
        }
    }

    enum Token {
        DIGIT, BINARYOPERATION, OPBRACKET, CLBRACKET, EMPTY, SIGN;


        Token update(StringBuilder t) {
            if (t.length() == 0)
                return EMPTY;
            char lastChar = t.charAt(t.length() - 1);
            if (Character.isDigit(lastChar) || lastChar == '.')
                return DIGIT;
            if (lastChar == '(')
                return OPBRACKET;
            if (lastChar == ')')
                return CLBRACKET;
            if (lastChar == '-' && t.length() > 1 && t.charAt(t.length() - 2) == '(')
                return SIGN;
            return BINARYOPERATION;
        }
    }
}
