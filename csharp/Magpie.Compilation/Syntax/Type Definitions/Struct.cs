﻿using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace Magpie.Compilation
{
    /// <summary>
    /// A structure type definition.
    /// </summary>
    public class Struct : Definition, INamedType
    {
        public readonly List<Field> Fields = new List<Field>();

        public Field this[string name] { get { return Fields.Find(field => field.Name == name); } }

        public Struct(Position position, string name) : base(position, name) { }

        public Struct(Position position, string name, IEnumerable<Field> fields)
            : base(position, name)
        {
            AddFields(fields);
        }

        public bool Contains(string name)
        {
            return Fields.Any(field => field.Name == name);
        }

        public void Define(string name, IUnboundDecl type)
        {
            Fields.Add(new Field(name, type, (byte)Fields.Count));
        }

        public void Define(string name, IBoundDecl type)
        {
            Fields.Add(new Field(name, type, (byte)Fields.Count));
        }

        public IEnumerable<ICallable> BuildFunctions()
        {
            yield return new StructConstructor(this);

            foreach (var field in Fields)
            {
                yield return new FieldGetter(this, field);
                yield return new FieldSetter(this, field);
            }
        }

        /// <summary>
        /// Creates a new deep copy of this structure in unbound form.
        /// </summary>
        public Struct Clone(IEnumerable<IBoundDecl> typeArguments)
        {
            var structure = new Struct(Position, BaseName,
                Fields.Select(field => new Field(field.Name,
                    field.Type.Unbound.Clone())));

            structure.BindSearchSpace(SearchSpace);
            structure.BindTypeArguments(typeArguments);

            return structure;
        }

        protected void AddFields(IEnumerable<Field> fields)
        {
            if (fields != null)
            {
                byte index = 0;
                foreach (var field in fields)
                {
                    Fields.Add(field);
                    field.SetIndex(index++);
                }
            }
        }

        #region IBoundDecl Members

        TReturn IBoundDecl.Accept<TReturn>(IBoundDeclVisitor<TReturn> visitor)
        {
            return visitor.Visit(this);
        }

        #endregion
    }

    public class Field
    {
        public string Name;
        public readonly Decl Type;
        public byte Index { get; private set; }

        public Field(string name, IUnboundDecl type)
            : this(name, type, 0) { }

        public Field(string name, IUnboundDecl type, byte index)
            : this(name, index)
        {
            Type = new Decl(type);
        }

        public Field(string name, IBoundDecl type, byte index)
            : this(name, index)
        {
            Type = new Decl(type);
        }

        private Field(string name, byte index)
        {
            Name = name;
            Index = index;
        }

        public void SetIndex(byte index)
        {
            Index = index;
        }
    }
}
