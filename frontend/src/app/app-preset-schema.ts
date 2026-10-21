import { definePreset } from '@primeng/themes';
import Material from '@primeng/themes/material';

const preset = definePreset(Material, {
    semantic: {
      colorScheme: {
        primary: {
          50: '{slate.50}',
          100: '{slate.100}',
          200: '{slate.200}',
          300: '{slate.300}',
          400: '{slate.400}',
          500: '{slate.500}',
          600: '{slate.600}',
          700: '{slate.700}',
          800: '{slate.800}',
          900: '{slate.900}',
          950: '{slate.950}'
        },
        secondary: { color: '#d62828', contrastColor: '#FFFFFF' },
        accent:    { color: '#f77f00', contrastColor: '#FFFFFF' },
        success:   { color: '{green.500}', contrastColor: '#FFFFFF' },
        warn:      { color: '{yellow.600}', contrastColor: '#000000' },
        error:     { color: '{red.600}',   contrastColor: '#FFFFFF' },
      }
    },
  },
);

export default preset;
