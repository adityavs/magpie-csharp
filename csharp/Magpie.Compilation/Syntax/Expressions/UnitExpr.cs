﻿using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    /// <summary>
    /// The Unit expression. Simply evaluates to Unit. Used in places where an "empty" expression
    /// is desired, for example a while/do loop that should repeatedly evaluate the condition,
    /// but has no need to do anything in the body.
    /// </summary>
    public class UnitExpr : IUnboundExpr, IBoundExpr
    {
        public Position Position { get; private set; }

        public UnitExpr(Position position)
        {
            Position = position;
        }

        public TReturn Accept<TReturn>(IUnboundExprVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }

        public IUnboundExpr AcceptTransformer(IUnboundExprTransformer transformer)
        {
            return transformer.Transform(this);
        }

        public override string ToString() { return "()"; }

        public IBoundDecl Type { get { return Decl.Unit; } }

        public TReturn Accept<TReturn>(IBoundExprVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }
    }
}
