<template>
  <!-- Default-Slot enthält die Tiles -->
  <div class="chs_grid">
    <slot />
  </div>
</template>

<script setup lang="ts">
// Keine Logik nötig – reines Grid
</script>

<style scoped>
/* 12-Spalten-Raster – 4x Small (3) / 2x Large (6) */
.chs_grid{
  --cols: 12; --span_small: 3; --span_large: 6; --gap: 20px;
  display: grid;
  grid-template-columns: repeat(var(--cols), minmax(0, 1fr));
  gap: var(--gap);
  width: 100%;
  align-content: start;
}

/* Hilfsklassen (auf Eltern-Wrapper der Tiles anwenden) */
:deep(.is_small){ grid-column: span var(--span_small); }
:deep(.is_large){ grid-column: span var(--span_large); }

/* HD ≤1280: 2 small (3 von 6), 1 large (6 von 6) pro Zeile */
@media (max-width: 1280px){
  .chs_grid{ --cols: 6; --span_small: 3; --span_large: 6; --gap:16px; }
}

/* Full-HD 1280–1920: 4 small / 2 large pro Zeile */
@media (min-width: 1280px) and (max-width: 1920px){
  .chs_grid{ --cols: 12; --span_small: 3; --span_large: 6; --gap:20px; }
}

/* 2K ≥2560: gleiche Anordnung, etwas mehr Luft */
@media (min-width: 2560px){
  .chs_grid{ --cols: 12; --gap:24px; }
}

/* optionale Mindesthöhen – verhindern „Zusammenschieben“ */
:deep(.chs_tile__small){ min-height: 180px }
:deep(.chs_tile__large){ min-height: 340px }
</style>
