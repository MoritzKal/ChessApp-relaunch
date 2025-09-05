<template>
  <div class="help">
    <h2>Hilfe & Beispiele</h2>

    <section>
      <h3>Mein erstes Dataset</h3>
      <ol>
        <li>Auf chess.com Ihre Partien als PGN exportieren.</li>
        <li>In der App auf <code>/datasets/import</code> gehen und Datei hochladen.</li>
        <li>Import prüfen und Stichprobe in der Dataset-Ansicht kontrollieren.</li>
      </ol>
    </section>

    <section>
      <h3>Erstes Training</h3>
      <ol>
        <li><code>/config/training</code> öffnen, Hyperparameter ausfüllen.</li>
        <li>Training starten und Fortschritt im Dashboard beobachten.</li>
        <li>Monitoring verlinken: RPS/Latency in Observability prüfen.</li>
      </ol>
    </section>

    <section>
      <h3>Gegen die KI spielen</h3>
      <ol>
        <li><code>/play</code> öffnen und eine Partie starten.</li>
        <li>Optionen in <code>/play/config</code> setzen (Temperature, Top-K).</li>
        <li>Über „PGN exportieren“ die Partie speichern.</li>
      </ol>
    </section>

    <section>
      <h3>Häufige Fehler</h3>
      <ul>
        <li><strong>401/403</strong>: Token abgelaufen oder fehlt. Neu anmelden.</li>
        <li><strong>404/410</strong>: Ressource nicht gefunden. URL prüfen.</li>
        <li><strong>5xx</strong>: Serverfehler. Später erneut versuchen; Logs/Observability prüfen.</li>
      </ul>
    </section>
  </div>
</template>

<script setup lang="ts">
// Static help content; if docs are available, this can be replaced with markdown fetch.
</script>

<style scoped>
.help{ max-width: 900px; margin: 0 auto; padding: 12px 8px }
code{ background: #ffffff14; padding: 1px 6px; border-radius: 4px }
ol{ padding-left: 20px }
ul{ padding-left: 18px }
h2{ margin-bottom: 12px }
section{ margin: 14px 0 }
</style>

