# NOTE: scattered thoughts

This file contains scattered thoughts / rough edits from several years of
writing about the problem that Snowflake intends to solve. I am keeping these
thoughts in this file for editing the main README later.

## The Problem

## The Solution

## The Tools

## FAQ

## Don't Overthink It

The underlying idea behind Snowflake is to treat your style definitions as a
data structure, specifically a hash map of hash maps. The syntax of `.flakes`
files is simple and reflects the underlying idea of treating style as a data
structure. If you find yourself working in a Snowflake project and wishing for
more power in your selectors, just make a new class and add that complexity to
your HTML template.

When in doubt, make a new class and let the tooling sort out the rest.

## When in doubt, add a new class

The cost to adding a new style definition is cheap and there is effectively no
penalty for adding new ones.

## Easily share styles

## Things that are broken in CSS currently

* tight coupling of DOM structure and selectors
* complex rules for selector specificity
* inability to analyze CSS with HTML due to logic "trapped" in templates; most
  things require visually inspecting in the browser
* layout properties are complected with style properties
* unused selectors and styles are practically impossible to detect by an algorithm
* hard to share libraries without clobbering existing styles. building on top of
  a library is difficult to "undue"

## Things that Snowflake fixes

* no need to worry about selector specificity
* no need to worry about DOM structure
* easily share and import other Snowflake libraries; use only the styles you
  reference in your HTML
* dead styles are removed programmatically
* tooling can extract the common elements of your styles for you, ie: DRY is
  done by an algorithm, not a human

## Soundbites / Misc thoughts

* When in doubt, just make a new class. The tooling will take care of the rest.
* Snowflake is slower to write up front and faster in the long run.
* Simple is better than complex, even if it costs more up front.
* CSS pre-processors like SCSS and LESS allow for easy writing of complex CSS;
  another way to think about this is that these tools let you write spaghetti
  code faster than using vanilla CSS
* How to deal with pseudo-classes and other sometimes necessary selectors? Provide
  an escape hatch to deal with these?
* No existing CSS system lets you visualize the connection between your CSS and
  markup. Snowflake enables this.

## Summary

UUCSS is a strategy and specification for dealing with the complexity of CSS in
large web applications and websites. It is designed to make CSS simple to reason
about and easy to maintain over time.

By using a flat structure of globally unique single-class selectors, UUCSS
avoids a host of problems caused by existing CSS techniques and enables the
creation of maintenance and optimization tools.

## The Problem

CSS on the web is a mess. Aside from the ever-growing list of browser-specific
property names and varying levels of support, layout difficulties, there are no
consistent standards for how to write and maintain a large CSS codebase. There
are a few attempts at a linting tool and some "style guide" documents produced
by a handful of companies, but industry-wide adoption of techniques and best
practices remains elusive in 2014.

This is a bizarre practice compared to much other web development. Most
programming languages come with either industrial grade linting programs and a
host of best practices for how to write readable and maintainable code. TODO:
link to a number of books on JavaScript best practices, style guides, linting
tools, also for other languages.

Here are some of the problems encountered by developers new to CSS:

* Bizarre, regular expression-like syntax for selectors.
* Large number of property names for new developers to learn. This list grows
  over time and shows no signs of slowing down.
* Varying levels of browser support for properties. Also some properites work
  some of the time, and in non-obvious ways (positioning).
* Some basic designs are still very difficult to achieve and non-obvious to
  implement.

Experienced CSS developers have problems too:

* Selector specificity is a crapshoot. Adding a CSS library can alter your site
  in small, non-obvious ways.
* CSS code tends to grow over time as everyone's strategy is basically "add
  your rule at the bottom of the file, test to make sure it gets applied (ie:
  wins the specificity war against all the other rules in your codebase)"
* Difficult to refactor / remove old CSS because you don't know what other
  cascading rules depend on it.
* The CSS rules and the HTML live in different places and sometimes even across
  teams. Did the latest feature update change the markup? Can I trust that class
  is still in the template file and didn't change?


## The Solution

By using a subset of selectors at a single specificity level we avoid an entire
class of problems related to the complexity of CSS specificity rules.

CSS itself is remarkable simple language. It consists of rules, which contain
selectors and a definition body. The definition body consists of property names
and values. (TODO: this should be an image with sections highlighted, not text)

CSS is fundamentally complected with the markup it is written to style. A single
CSS rule is potentially complected with the following:

* The structure of the HTML document.
* The tags, classes, ids, and attributes used in the selector.
* Other CSS rules.

Fortunately for web developers, CSS parsing and style application is one of the
fastest things a browsers does when loading a webpage. As a result, CSS
performance is rarely a concern for the vast majority of web projects.

What *is* a concern for web developers is the growth of CSS over the lifetime of
a project.

All web developers have to do is avoid a handful of selectors with poor
performance characteristics and the chances of their CSS being their limiting
factor for speed is practically non-existent.

## Understanding CSS

Before we get into the benefits of UUCSS it helps to take a fresh look at CSS
and discuss some of it's properties.

At it's core, CSS consists of selectors and style definitions. Selectors are
strings that describe what DOM elements to target with style. Style definitions
consist of key / value pairs.

Suppose I am a web developer looking at the following chunk of CSS:

```
TODO: find a complex selector from Bootstrap
.foo > p:first {

}
```

What questions might I have about this particular chunk of code?

* What does this look like visually in the browser?
* How often is this style applied / used throughout the application?
* Does this selector target a lot of HTML or just a little piece?
* Where is the HTML this selector is intended to target in the rest of the
  codebase? Which template files, static HTML, JavaScript, etc?
* Is this selector specific enough to apply the style? Is it overly specific?
* Is this selector used by JavaScript in any way?
* Will this rule be overwritten by a different selector? If so, which properties
  will be overwritten and which ones will apply? Where is the selector or
  selectors that will override this one?
* Does this selector target *any* HTML in my web application? Is this dead code?

None of these questions are easy to answer and depending on how your application
is structured some questions may be difficult or impossible to answer.

## A Possible Solution

A flat structure of uniquely-named selectors all at the same level allows for
answers to most of our questions:

* No possibility of selector clashes between rules.
* All of the properties of the rule get applied 100% to the element selected. No
  mix and match between multiple style definitions.
* Ability to find corresponding HTML anywhere in the codebase. ie: easy grepping
* Programmatic removal of unused selectors. ie: dead code removal
* No concern about breaking style located somewhere else in the codebase.
* Simple element targeting for JavaScript events.
* Safely include a UUCSS stylesheet into your project without concern for
  clashing with other styles. Unused classes are easy to remove via tools.
* No need to know all of the selector rules and specificity levels.


Imagine the following chunk of code if you knew that every CSS selector
consisted of a single class selector and was uniquely named:

```
TODO: example goes here
```

Some of the questions from the previous example remain, such as "What does this
look like visually in the browser?". The rest either don't apply or have simple
answers via tooling.

You can start using UUCSS with your project today. Any UUCSS class names should
be unique enough not to clash with existing style rules and you can confidently
add a UUCSS stylesheet to your project without worrying about breaking existing
styles (unless your markup has exactly the classes referenced in the included
sheet).

TODO: need a picture of a CSS rule, with callouts that show what is a rule, what
is a selector, and what is a declaration

## UUCSS Specification

The key words "MUST", "MUST NOT", "REQUIRED", "SHALL", "SHALL NOT", "SHOULD",
"SHOULD NOT", "RECOMMENDED", "MAY", and "OPTIONAL" in this document are to be
interpreted as described in [RFC 2119](http://tools.ietf.org/html/rfc2119).

1. A UUCSS class name MUST consist of lower-case alphanumeric words separated by
hyphens and ending with a 5 character "hash". The hash part MUST consist of only
hexadecimal characters (ie: [abcdef0-9]) and MUST contain at least one digit
character and one alpha character.

1. A UUCSS class name MUST be a globally unique identifier for the project it is
being used on. A UUCSS class name's uniqueness SHOULD be confirmed upon it's
creation by use of a code-searching tool. (ie: grep, awk, etc)

1. UUCSS rules SHOULD be treated as immutable values (ie: "write once"). Once a
UUCSS rule has been created, the value of it's declaration SHOULD NOT change
with any future code change. If a change in the declaration of an existing UUCSS
rule is desired, a new UUCSS class name and rule SHOULD be created.

1. A UUCSS rule SHOULD only contain a single-class selector consisting of a
single UUCSS class name. It is NOT RECOMMENDED to use UUCSS class names in rules
that are not single-class selectors.

1. UUCSS class names MAY be used for pseudo-class rules.

1. UUCSS rules SHOULD only be defined once in the project.

1. HTML elements SHOULD only contain one UUCSS class. If multiple UUCSS classes
are desired on the same element, a new UUCSS rule SHOULD be created.

1. The use of tools SHOULD be used in order to determine which UUCSS rules can
be safely deleted, which UUCSS rules can be combined.








## FAQ

### What about inheritance and the cascade? It is "cascading" stylesheets after
    all.

In the case of non-layout properties (ie: font-family, font-size, color) use the
natural inheritance of the DOM structure (you have no choice).

For properties that do not inherit from the DOM structure, use the powerful
abstractions provided by pre-processor tools (ie: LESS, SASS, etc). Better to
compose functions against a simple data model than to create complex data.

Functions and variables are a powerful tool for creating composable inheritance
models and allow for a cleaner abstraction than the invisible, DOM-complected
cascading model of CSS.

http://nicolasgallagher.com/css-cascade-specificity-inheritance/

### What about semantic class names?

Markup is intended to be semantic; class names do not have to be.

Being semantic in the naming of UUCSS classes may beneficial to help the
developer understand the context in which a UUCSS class is to be used, but it
shouldn't be confused with the value and purpose of semantic HTML.

### I'm doing UUCSS, but my class names aren't always winning in specificity
    over some external CSS library. What can I do?

Unfortunately, there's not a great "UUCSS" solution to this because the problem
lies with overly-specific rules in another sheet. Recommend either removing that
sheet from your project or politely convincing it's author to switch to UUCSS :)

### Won't this add many more classes in my HTML than necessary?

Yes. UUCSS projects generally have more HTML classes than other CSS techniques.
This is expected and is a side effect of the simplification of CSS selector
rules being used. UUCSS prefers lots of small classes that are *equally
specific* and *simple to reason about* over a complex mix of selectors at
various specificity levels that are dependent on the DOM structure they are
intended to target.

"It is better to have 100 functions operate on one data structure than to have
 10 functions operate on 10 data structures." - Alan J. Perlis

### What about performance? Won't UUCSS make my files much larger? What about
    class selector performance?

UUCSS was primarily designed for developer simplicity and long-term CSS
maintenance concerns.

That being said, it has great performance anyway. TODO: more here

### You want me to use a single class selector for *everything*? What about ids
    that only exist once?

It is generally not recommended to use ids as selectors due to their high
selector specificity as well as the difficulty in guaranteeing their uniqueness
on the page of a large web application.

### This is a great theory and all, but does it work in practice?

UUCSS was developed on real-world projects at a publicly-traded company.









### Misc

CSS selectors are complected with DOM content and structure. DOM content = tags,
classes, IDs, etc.

Dom content and structure are best handled by the full power of a Turing
complete programming language. ie: your templating language

UUCSS seeks to de-couple style definition from DOM content and structure

Mixins must be capitalized in .uucss files
@sign in front of variables

How to handle @import / shared variables / shared Mixins in .uucss files?








### UUCSS - those strange CSS hashes

When looking through the code, you will undoubtedly notice strange hashes
appended to all the CSS class names (e.g. `.json-link-0c551`)

This is an invention by Chris Oakman for simplifying CSS. It simplifies
CSS by forcing you to style your markup with a flat list of Universally Unique
class names.

By itself, CSS styling is dependent on four things:

1. CSS statement order
2. CSS nesting/specificity
3. CSS file inclusion order in an HTML page
4. The position of the element in the DOM tree

But adhering to a flat list of Universally Unique class names removes the first
three problems completely. This enables styling to be dependent on one thing
only, the DOM.  UUCSS becomes easier to reason about.

Using UUCSS has additional benefits:

- class names are easily locatable via simple file grepping
- we can mix UUCSS stylesheets from separate products with confidence
- dead/unused UUCSS classes are easily detectable via automated purging/minifying

Furthermore, treating UUCSS classes as immutable values when working in a team
can ensure that we don't break each other's code.  New UUCSS classes can be
easily cloned and modified from previous classes to save us from worrying about
breaking changes.

A UUCSS website with a formal spec and tooling is currently being developed for
public review and consumption.
