# Validierung, Fehler, UX-Standards

* **Validation**: zod-Schemas pro Request (Client-Side), Server-Fehler → menschenlesbar mappen.
* **Error-States**: Card-Level Fehler mit „Retry“ + „View Logs“. Globaler `onAxiosError` → Snackbar.
* **Leere Zustände**: Handlungsaufforderungen (Import starten / Dataset bauen / Training starten).
* **A11y/Contrast**: Messing-Akzente für Headlines/KPIs, Fließtext in `on-surface`.
