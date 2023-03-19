<template>
  <input type="checkbox" id="terminal-modal" class="modal-toggle" />
  <label for="terminal-modal" class="modal cursor-pointer">
    <label class="modal-box w-11/12 max-w-5xl h-full" for="">
      <div id="terminal" ref="terminal"></div>
    </label>
  </label>
</template>

<script setup lang="ts">
import type { Ref } from "vue";
import { onMounted, ref } from "vue";
import { Terminal } from "xterm";
import { FitAddon } from "xterm-addon-fit";
import { WebglAddon } from "xterm-addon-webgl";
import { debounce } from "lodash-es";

// @ts-ignore
const terminal: Ref<HTMLElement> = ref(null);
const term = new Terminal({
  convertEol: true,
  disableStdin: true,
});

const fitAddon = new FitAddon();
term.loadAddon(fitAddon);
term.loadAddon(new WebglAddon());

onresize = () => {
  fitTerm();
};

onMounted(() => {
  console.log(terminal.value);
  term.open(terminal.value);
  fitTerm();
  connectIO();
});

const fitTerm = debounce(() => {
  fitAddon.fit();
}, 150);

const connectIO = () => {
  const socket = new WebSocket("ws://127.0.0.1:17555/io");
  socket.onopen = () => {
    console.log("Connected to WAI2K IO");
  };
  socket.onmessage = async (event) => {
    term.write(event.data);
  };
  socket.onclose = () => {
    console.log("Disconnected from WAI2K IO, reconnecting...");
    setTimeout(() => connectIO(), 1000);
  };
};
</script>
