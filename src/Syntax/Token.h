#pragma once

#include <iostream>

#include "Macros.h"
#include "MagpieString.h"

namespace magpie
{
  class Memory;

  // The different types of Tokens that make up Magpie source code.
  enum TokenType
  {
    // Punctuators.
    TOKEN_LEFT_PAREN,
    TOKEN_RIGHT_PAREN,
    TOKEN_LEFT_BRACKET,
    TOKEN_RIGHT_BRACKET,
    TOKEN_LEFT_BRACE,
    TOKEN_RIGHT_BRACE,
    TOKEN_EQUALS,
    TOKEN_PLUS,
    TOKEN_MINUS,
    TOKEN_STAR,
    TOKEN_SLASH,
    TOKEN_PERCENT,
    TOKEN_LESS_THAN,
    
    // Keywords.
    TOKEN_AND,
    TOKEN_CASE,
    TOKEN_DEF,
    TOKEN_DO,
    TOKEN_END,
    TOKEN_ELSE,
    TOKEN_FALSE,
    TOKEN_FOR,
    TOKEN_IF,
    TOKEN_IS,
    TOKEN_MATCH,
    TOKEN_NOT,
    TOKEN_OR,
    TOKEN_RETURN,
    TOKEN_THEN,
    TOKEN_TRUE,
    TOKEN_VAL,
    TOKEN_VAR,
    TOKEN_WHILE,
    TOKEN_XOR,

    TOKEN_NAME,
    TOKEN_NUMBER,
    TOKEN_STRING,

    TOKEN_LINE,
    TOKEN_ERROR,
    TOKEN_EOF,
    
    TOKEN_NUM_TYPES
  };

  // A single meaningful Token of source code. Generated by the Lexer, and
  // consumed by the Parser.
  class Token : public Managed
  {
  public:
    static temp<Token> create(TokenType type, gc<String> text);

    static const char* typeString(TokenType type);
    
    TokenType        type() const { return type_; }
    const gc<String> text() const { return text_; }
    
    // Gets whether this token is of the given type.
    bool is(TokenType type) const { return type_ == type; }
    
    virtual void reach();
    virtual void trace(std::ostream& out) const;
    
  private:
    Token(TokenType type, const gc<String> text);

    TokenType   type_;
    gc<String>  text_;
  };
}

