/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ["./jvm/src/main/resources/html/*.{html,js}", "./jvm/src/main/scala/fi/kimmoeklund/html/Tailwind.scala"],
theme: {
  extend: { },
},
plugins: [
  require('@tailwindcss/forms'),
],
}

