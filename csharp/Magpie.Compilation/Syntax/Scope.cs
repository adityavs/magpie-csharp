﻿using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    /// <summary>
    /// A local variable lexical scope.
    /// </summary>
    public class Scope
    {
        /// <summary>
        /// Gets the number of variable slots needed for this scope. This is the
        /// maximum number of variables that have been defined at one time.
        /// </summary>
        public int NumVariables { get { return mNumVariables; } }

        /// <summary>
        /// Gets the local variable with the given name.
        /// </summary>
        /// <param name="name">Name of the local variable to look up.</param>
        /// <returns>The Field representing the local variable.</returns>
        public Field this[string name] { get { return mStruct[name]; } }

        /// <summary>
        /// Starts a new inner scope.
        /// </summary>
        public void Push()
        {
            // remember where this scope starts
            mInnerScopes.Push(mStruct.Fields.Count);
        }

        /// <summary>
        /// Closes this scope and discards locals defined in it.
        /// </summary>
        public void Pop()
        {
            // forget every local defined in the inner scope
            int start = mInnerScopes.Pop();
            if (start < mStruct.Fields.Count)
            {
                mStruct.Fields.RemoveRange(start, mStruct.Fields.Count - start);
            }
        }

        /// <summary>
        /// Gets whether or not the scope currently has a local variable with
        /// the given name.
        /// </summary>
        /// <param name="name">Name to look for.</param>
        /// <returns><c>true</c> if a local variable with that name is in scope.</returns>
        public bool Contains(string name)
        {
            return mStruct.Contains(name);
        }

        /// <summary>
        /// Gets whether or not the variable with the given name is mutable.
        /// </summary>
        /// <param name="name">The name of the variable.</param>
        /// <returns><c>true</c> if the variable is mutable.</returns>
        public bool IsMutable(string name)
        {
            return mVariableMutability[name];
        }

        /// <summary>
        /// Defines a local variable with the given name and type.
        /// </summary>
        /// <param name="name">Variable name.</param>
        /// <param name="type">Variable type.</param>
        /// <param name="isMutable"><c>true</c> if the variable is mutable.</param>
        public void Define(string name, IBoundDecl type, bool isMutable)
        {
            mStruct.Define(name, type);

            mVariableMutability[name] = isMutable;

            // track the highwater
            mNumVariables = Math.Max(mNumVariables, mStruct.Fields.Count);
        }

        private readonly Struct mStruct = new Struct(Position.None, "_scope", null);
        private readonly Stack<int> mInnerScopes = new Stack<int>();
        private readonly Dictionary<string, bool> mVariableMutability = new Dictionary<string,bool>();
        private int mNumVariables = 0;
    }
}
