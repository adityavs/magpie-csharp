﻿using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Linq;
using System.Text;

using Magpie.Compilation;
using Magpie.Interpreter;

namespace Magpie.App
{
    public class TestSuite
    {
        public TestSuite(string testDir)
        {
            mTestDir = testDir;
        }

        public void Run()
        {
            int passed = 0;
            int failed = 0;
            int skipped = 0;

            foreach (string test in Directory.GetFiles(mTestDir, "*.mag", SearchOption.AllDirectories))
            {
                try
                {
                    if (!RunTest(test)) skipped++;
                }
                catch (Exception ex)
                {
                    mErrors.Add("interpreter threw \"" + ex.Message + "\"");
                }

                string relativePath = test.Replace(mTestDir, "");
                if (mErrors.Count == 0)
                {
                    //Console.WriteLine("  pass " + relativePath);
                    passed++;
                }
                else
                {
                    Console.WriteLine("! FAIL " + relativePath);
                    failed++;

                    foreach (string error in mErrors)
                    {
                        Console.WriteLine("       " + error);
                    }
                }
            }

            Console.WriteLine();
            Console.WriteLine("passed {0}/{1} tests ({2} skipped)", passed, passed + failed, skipped);
        }

        private bool RunTest(string test)
        {
            mErrors = new List<string>();

            if (!ParseExpected(test))
            {
                // script is disabled
                return false;
            }

            Script script = new Script(test);

            script.Printed += TestPrint;
            script.MaxStackDepth = mMaxStackDepth;

            script.Run(String.Empty);

            script.Printed -= TestPrint;

            IList<CompileError> compileErrors = script.Errors;

            if (compileErrors.Count == 0)
            {
                while (mExpectedOutput.Count > 0)
                {
                    mErrors.Add("out of output while expecting \"" + mExpectedOutput.Dequeue() + "\"");
                }

                while (mExpectedErrorLines.Count > 0)
                {
                    int expectedLine = mExpectedErrorLines.Dequeue();
                    mErrors.Add("Expected compile error on line " + expectedLine + " but got none.");
                }
            }
            else
            {
                foreach (var error in compileErrors)
                {
                    if (mExpectedErrorLines.Count > 0)
                    {
                        int expectedLine = mExpectedErrorLines.Dequeue();
                        if (error.Position.Line != expectedLine)
                        {
                            mErrors.Add("Expected compile error on line " + expectedLine + " but was on line " + error.Position.Line);
                            mErrors.Add(error.Message);
                        }
                    }
                    else
                    {
                        mErrors.Add("Got compile error on line " + error.Position.Line + " when no more were expected.");
                        mErrors.Add(error.Message);
                    }
                }
            }

            return true;
        }

        private bool ParseExpected(string path)
        {
            mExpectedOutput.Clear();
            mExpectedErrorLines.Clear();
            mMaxStackDepth = 0;
            bool enabled = true;

            const string ExpectedHeader    = "// expected: ";
            const string ErrorHeader       = "// error line: ";
            const string MaxStackHeader    = "// stack limit: ";
            const string Disable           = "// disable";

            foreach (string line in File.ReadAllLines(path))
            {
                if (line.StartsWith(ExpectedHeader))
                {
                    mExpectedOutput.Enqueue(line.Substring(ExpectedHeader.Length));
                }
                else if (line.StartsWith(ErrorHeader))
                {
                    string errorLine = line.Substring(ErrorHeader.Length).Trim();
                    mExpectedErrorLines.Enqueue(Int32.Parse(errorLine));
                }
                else if (line.StartsWith(MaxStackHeader))
                {
                    string stackLine = line.Substring(MaxStackHeader.Length).Trim();
                    mMaxStackDepth = Int32.Parse(stackLine);
                }
                else if (line.StartsWith(Disable))
                {
                    enabled = false;
                }
            }

            return enabled;
        }

        private void TestPrint(object sender, PrintEventArgs e)
        {
            if (mExpectedOutput.Count == 0)
            {
                mErrors.Add("got \"" + e.Text + "\" when not expecting any more output");
            }
            else
            {
                string expecting = mExpectedOutput.Dequeue();
                string actual = e.Text.Replace("\n", "\\n");

                if (expecting != actual)
                {
                    mErrors.Add("got \"" + actual + "\" when expecting \"" + expecting + "\"");
                }
            }
        }

        private string mTestDir;
        private Queue<string> mExpectedOutput = new Queue<string>();
        private Queue<int> mExpectedErrorLines = new Queue<int>();
        private int mMaxStackDepth;
        private List<string> mErrors;
    }
}
