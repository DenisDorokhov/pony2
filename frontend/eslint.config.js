// @ts-check
const eslint = require("@eslint/js");
const tseslint = require("typescript-eslint");
const angular = require("angular-eslint");

module.exports = tseslint.config(
  {
    files: ["**/*.ts"],
    extends: [
      eslint.configs.recommended,
      ...tseslint.configs.recommended,
      ...tseslint.configs.stylistic,
      ...angular.configs.tsRecommended,
    ],
    processor: angular.processInlineTemplates,
    rules: {
      "@angular-eslint/directive-selector": [
        "error",
        {
          type: "attribute",
          prefix: "pony",
          style: "camelCase",
        },
      ],
      "@angular-eslint/component-selector": [
        "error",
        {
          type: "element",
          prefix: "pony",
          style: "kebab-case",
        },
      ],
      "@typescript-eslint/no-explicit-any": "off",
      "@typescript-eslint/no-namespace": "off",
      "semi": "error",
      "quotes": [2, "single", { "avoidEscape": true, "allowTemplateLiterals": true }],
    },
  },
  {
    files: ["**/*.html"],
    extends: [
      ...angular.configs.templateRecommended,
      ...angular.configs.templateAccessibility,
    ],
    rules: {
      "@angular-eslint/template/click-events-have-key-events": "off",
      "@angular-eslint/template/label-has-associated-control": "off",
      "@angular-eslint/template/interactive-supports-focus": "off",
    },
  }
);
