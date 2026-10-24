<!-- Copyright (c) Cratis. All rights reserved. -->
<!-- Licensed under the MIT license. See LICENSE file in the project root for full license information. -->

# Casing fixtures

Copied from [Fundamentals.Go case.json](https://github.com/Cratis/Fundamentals.Go/blob/c04690df426c5383b62dbf6c71bd799530d5bf0a/naming/testdata/case.json), hand-derived from C# source and specifications. `source` paths are relative to the C# repository. `revision` pins [Fundamentals f87af60](https://github.com/Cratis/Fundamentals/tree/f87af60265a2e59bd9b7435b0386ee06cb6d646a).

`git diff d2accc4 f87af60 -- Source/DotNET/Fundamentals/Strings/StringExtensions.cs Source/DotNET/Fundamentals.Specs/Strings/for_ToCamelCase/when_converting.cs` was empty. Removed all three Go-only `inputHex` byte rows and added the original C# unpaired UTF-16 surrogate case, which JVM strings preserve directly. Every retained row tests both camel and pascal casing.

The [invariant casing implementation](https://github.com/dotnet/runtime/blob/v10.0.0/src/native/libs/System.Globalization.Native/pal_casing.c#L65-L109) preserves U+0130 on lowercase and U+0131 on uppercase. These exceptions differ from JDK defaults. The fixture pins selected Unicode characters, not all platform or Unicode-version casing tables.
