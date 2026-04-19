/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{vue,ts,js}"],
  theme: {
    extend: {
      screens: {
        desk: "1000px",
      },
      colors: {
        fm: {
          white: "#FFFFFF",
          stone: "#F5F2ED",
          gold: "#C9A96E",
          black: "#1A1A1A",
          gray: "#BCB4A8",
          red: "#E03A00",
        },
        tier: {
          1: "#C9A96E",
          2: "#E03A00",
          3: "#BCB4A8",
        },
      },
      fontFamily: {
        logo: ['"Tiller"', 'Georgia', 'serif'],
        sans: ['"Univers"', '"Helvetica Neue"', 'Arial', 'sans-serif'],
        serif: ['"Adobe Garamond"', 'Georgia', 'serif'],
      },
      maxWidth: {
        mobile: "640px",
      },
    },
  },
  plugins: [],
};
