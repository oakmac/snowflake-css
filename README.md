# Snowflake CSS **[WORK IN PROGRESS]**

## Project Status

As of September 2016, this project is in a very early stage of development and
should not be considered usable.

## Summary

Snowflake CSS is a strategy and set of tools designed for dealing with the
complexity of CSS in large web applications and websites. It is designed to make
CSS simple to reason about and easy to maintain over time.

By using a flat structure of globally unique single-class selectors (flakes),
Snowflake CSS avoids a host of problems caused by existing CSS techniques and
enables maintenance and optimization tasks to be done with tools instead of
developer brainpower.

## Development

Install [Leiningen] and [Node.js].

```sh
## install node_modules
npm install

## compile CLJS files
lein clean && lein cljsbuild auto

## run node.js server
node app.js
```

## License

[ISC License]

[Leiningen]:http://leiningen.org
[Node.js]:http://nodejs.org
[ISC License]:LICENSE.md
