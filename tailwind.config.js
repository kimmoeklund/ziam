/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ["./jvm/src/main/resources/html/*.{html,js}"],
  theme: {
    extend: {},
  },
  plugins: [
    require('@tailwindcss/forms'),
  ],
}

