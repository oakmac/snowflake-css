# Snowflake CSS

**tldr:** Simplify CSS with a globally unique hash-map for selectors.

## What is all this about?

Snowflake CSS is a code strategy and set of tools designed for dealing with the
complexity of CSS in large web applications and websites. It is designed to make
CSS simple to reason about and easy to maintain over time.

By using a flat structure of globally unique single-class selectors (flakes),
Snowflake CSS avoids complexity often found in large CSS codebases and enables
maintenance and optimization tasks to be done with tools instead of developer
brainpower.

## How do I use Snowflake CSS?

The rules for Snowflake are simple:

1. Use only single-class selectors.
1. Use globally-unique symbols for your class names.
1. Do not change your selectors over time (ie: append-only CSS)

## How do you create a globally unique class name?

Snowflake CSS uses the following convention for class names:

1. only lower-case letters with hyphens (ie: `snake-case`)
1. append a 5-digit hash to the end of the class name:
  - the hash must be hexadecimal characters (ie: `[a-f0-9]`)
  - the hash must have at least **one number character**
  - the hash must have at least **one alpha character**

Examples:
- `.primary-btn-d4b50`
- `.header-411db`
- `.login-btn-9c2da`
- `.cancel-6b36a`

## What is the deal with those hashes?

The hashes are random and only serve the purpose of creating a project-wide
unique symbol that can be found via tooling.

Using 5 hexademical characters creates an address space of around 1 million
options (16^5). Combined with the rest of the words in the class name should be
plenty enough randomness for even the largest of web projects.

The requirement of "at least one number and one letter" came from testing in
practical application. This helps to reduce false positive matches against
UUIDs, long numbers, and other symbols often found in codebases.

A list of 2000 valid hashes can be found [here](https://oakmac.com/hashes.php).
I keep a file on my desktop called `hashes.txt` and simply cut/paste one at a
time whenever I need to create a new class. I hope to create some
editor-integrated tooling to assist with creating, writing, copying class names
in the future. Any help here would be appreciated :)

## Getting Started

```sh
# add snowflake-css to your node.js project dependencies
npm install snowflake-css --save-dev

# initialize a snowflake-css.json file with your information
npx snowflake init

# remove orphan snowflake classes from your production CSS output
npx snowflake prune
```

## Development

Snowflake CSS is written in [ClojureScript] using [shadow-cljs].

```sh
# install node_modules
yarn install

# compile bin/snowflake.js
npx shadow-cljs release snowflake
```

[ClojureScript]:https://clojurescript.org/
[shadow-cljs]:https://shadow-cljs.org/

## License ![ISC License Badge](https://img.shields.io/badge/license-ISC-green)

[ISC License](LICENSE.md)
