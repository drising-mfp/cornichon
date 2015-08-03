cornichon [![Build Status](https://travis-ci.org/agourlay/cornichon.png?branch=master)](https://travis-ci.org/agourlay/cornichon)
=========

Scala DSL for testing JSON HTTP API

Work in progress...

See [example](https://github.com/agourlay/cornichon/blob/master/src/test/scala/com/github/agourlay/cornichon/examples/HttpExamplesSpec.scala) for usage.

// TODOs
- add test cases for DELETE
- build request abstraction
- call resolver on assertion
- errors with JSON Diff if possible
- JSON assertion ignoring fields
- data table
- string based DSL with parboiled2
- hook Before/After Feature
- hook Before/After Scenario
- use infix notation? Given GET() ...
- Improve scalatest integration, display step progression