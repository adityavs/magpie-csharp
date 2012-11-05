﻿using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    public interface IBoundExprVisitor<TReturn>
    {
        TReturn Visit(UnitExpr expr);
        TReturn Visit(BoolExpr expr);
        TReturn Visit(IntExpr expr);
        TReturn Visit(StringExpr expr);

        TReturn Visit(BoundFuncRefExpr expr);

        TReturn Visit(BoundTupleExpr expr);
        TReturn Visit(BoundBlockExpr expr);

        TReturn Visit(LoadExpr expr);
        TReturn Visit(StoreExpr expr);
        TReturn Visit(LocalsExpr expr);

        TReturn Visit(BoundCallExpr expr);
        TReturn Visit(IntrinsicExpr expr);
        TReturn Visit(ForeignCallExpr expr);

        TReturn Visit(BoundIfExpr expr);
        TReturn Visit(BoundReturnExpr expr);
        TReturn Visit(BoundWhileExpr expr);
    }
}
