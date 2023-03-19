/** @type {import('tailwindcss').Config} */
// eslint-disable-next-line no-undef
module.exports = {
  content: ["./index.html", "./src/**/*.{vue,js,ts,jsx,tsx}"],
  theme: {
    extend: {
      screens: {
        '3xl': '1920px',
        '4xl': '2400px',
        '5xl': '3000px'
      }
    }
  },
  // eslint-disable-next-line no-undef
  plugins: [require("daisyui")],
};
