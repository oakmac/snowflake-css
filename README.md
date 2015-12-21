# Snowflake CSS

## Summary

Snowflake CSS is a strategy and set of tools designed for dealing with the
complexity of CSS in large web applications and websites. It is designed to make
CSS simple to reason about and easy to maintain over time.

By using a flat structure of globally unique single-class selectors, Snowflake
CSS avoids a host of problems caused by existing CSS techniques and enables
maintenance and optimization tasks to be done with tools.

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

The cost to adding a new
style definition is cheap and there is practically no penalty for adding new
ones.

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

## License

[ISC License]

[ISC License]:LICENSE.md
