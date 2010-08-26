package com.stuffwithstuff.magpie.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import com.stuffwithstuff.magpie.ast.BlockExpr;
import com.stuffwithstuff.magpie.ast.Condition;
import com.stuffwithstuff.magpie.ast.Expr;
import com.stuffwithstuff.magpie.ast.IfExpr;
import com.stuffwithstuff.magpie.ast.NothingExpr;

public class ConditionalExprParser implements ExprParser {

  @Override
  public Expr parse(MagpieParser parser) {
    Position startPos = parser.current().getPosition();
    
    // If expressions have their condition wrapped in a truthiness test. This
    // lets you use arbitrary types in conditions and let the types themselves
    // determine what true means for them. They get desugared like this:
    //
    // if foo then
    //
    // To:
    //
    // if foo true? then
    //
    // Let expressions and multiple if conditions get desugared like this:
    //
    // let a = foo
    // let b = bar
    // if c = d then
    //     e
    // end
    //
    // To:
    //
    // var a__ = foo
    // if a__ != nothing then
    //     var a = OrType unsafeRemoveType(a__, Nothing)
    //     var b__ = bar
    //     if b__ != nothing then
    //         var b = OrType unsafeRemoveType(b__, Nothing)
    //         if c = d then
    //             e
    //         end
    //     end
    // end
    //
    
    // Parse the conditions.
    Stack<Condition> conditions = new Stack<Condition>();
    while (true) {
      if (parser.match(TokenType.IF)) {
        Expr condition = parser.parseIfBlock();
        conditions.add(new Condition(condition));
      } else if (parser.match(TokenType.LET)) {
        // TODO(bob): Eventually allow tuple decomposition here.
        String name = parser.consume(TokenType.NAME).getString();
        parser.consume(TokenType.EQUALS);
        conditions.add(new Condition(name, parser.parseIfBlock()));
      } else {
        break;
      }
    }
    
    // Parse the then body.
    parser.consume(TokenType.THEN);
    Expr thenExpr;
    boolean allowElse = true;
    if (parser.match(TokenType.LINE)){
      Position position = parser.last(1).getPosition();
      List<Expr> exprs = new ArrayList<Expr>();
      
      do {
        exprs.add(parser.parseExpression());
        parser.consume(TokenType.LINE);
      } while (!parser.lookAhead(TokenType.ELSE) && !parser.match(TokenType.END));
      
      // Can't have an else clause if the then clause ended with "end".
      if (parser.last(1).getType() == TokenType.END) {
        allowElse = false;
      }
      
      position = position.union(parser.last(1).getPosition());
      thenExpr = new BlockExpr(position, exprs);
    } else {
      thenExpr = parser.parseExpression();
    }

    // Parse the else body.
    Expr elseExpr = null;
    if (allowElse && (parser.match(TokenType.ELSE) ||
                      parser.match(TokenType.LINE, TokenType.ELSE))) {
      elseExpr = parser.parseElseBlock();
    } else {
      elseExpr = new NothingExpr(parser.last(1).getPosition());
    }
    
    // TODO(bob): For testing, compile simple if expressions to something that
    // just uses local functions.
    if ((conditions.size() == 1) && !conditions.get(0).isLet()) {
      Expr.message(null, "__if", Expr.tuple(
          conditions.get(0).getBody(),
          Expr.fn(thenExpr),
          Expr.fn(elseExpr)));
    }
    
    return new IfExpr(startPos.union(elseExpr.getPosition()),
        conditions, thenExpr, elseExpr);
  }
}