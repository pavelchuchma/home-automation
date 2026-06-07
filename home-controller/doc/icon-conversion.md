# Icon conversion notes

## PNG → SVG

Online tool used so far: **<https://convertio.co/png-svg/>**

Existing example: all `goat-*.svg` icons referenced from
`app/src/main/resources/servlet/content/items/robonectItem.js` (see the
comment `// images converted by https://convertio.co/png-svg/` near the
`onCanvasCreatedImpl()` setup).

Output is placed under
`app/src/main/resources/servlet/content/img/` and embedded via
`this.svg.image('img/<name>.svg')` (SVG.js API).
