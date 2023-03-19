<style>
@import "xterm/css/xterm.css";
</style>

<template>
  <main class="flex gap-4 h-full">
    <div class="flex flex-col grow">
      <LayoutHeader class="flex"/>
      <RouterView class="flex flex-1"/>
      <LayoutFooter class="flex"/>
    </div>
    <button class="bg-neutral-content" @click.prevent="toggleTerm()">
      <font-awesome-icon
          class="text-3xl transition-all duration-300"
          :class="appStore.showTerm ? 'rotate-0' : 'rotate-180'"
          icon="fa-solid fa-angle-right"
      />
    </button>
    <div
        class="overflow-x-scroll transition-all duration-300"
        :class="appStore.showTerm ? 'basis-1/3' : 'basis-0'"
    >
      <div id="term" class="h-full"/>
    </div>
    <div id="modals"></div>
    <div id="changes"></div>
  </main>
</template>

<script setup lang="ts">
import { RouterView } from "vue-router";
import LayoutHeader from "./components/LayoutHeader.vue";
import LayoutFooter from "./components/LayoutFooter.vue";
import { onMounted } from "vue";
import { useClassifierStore } from "@/stores/classifiers";
import { Terminal } from "xterm";
import { FitAddon } from "xterm-addon-fit";
import { useAppStore } from "@/stores/app";
import { debounce } from "lodash-es";

const classifierStore = useClassifierStore();
classifierStore.load();

const appStore = useAppStore();

const term = new Terminal({
  disableStdin: true,
});

const fitAddon = new FitAddon();
term.loadAddon(fitAddon);

onresize = () => {
  fitTerm();
};

onMounted(() => {
  term.open(document.getElementById("term")!);
  fitTerm();
  connectIO();
});

const toggleTerm = () => {
  appStore.showTerm = !appStore.showTerm;
  fitTerm();
};

const fitTerm = debounce(() => {
  fitAddon.fit();
}, 300);

const connectIO = () => {
  const socket = new WebSocket("ws://127.0.0.1:17555/io");
  socket.onopen = () => {
    console.log("Connected to WAI2K IO");
  };
  socket.onmessage = async (event) => {
    term.writeln(event.data);
  };
  socket.onclose = () => {
    console.log("Disconnected from WAI2K IO, reconnecting...");
    setTimeout(() => connectIO(), 1000);
  };
};
</script>
