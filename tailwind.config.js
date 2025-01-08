/** @type {import('tailwindcss').Config} */
module.exports = {
    content: ["./src/main/twirl/fi/kimmoeklund/templates/*.html", "./src/main/scala/fi/kimmoeklund/html/Tailwind.scala"],
    theme: {
        extend: {},
    },
    plugins: [
        require('@tailwindcss/forms'),
    ],
}

