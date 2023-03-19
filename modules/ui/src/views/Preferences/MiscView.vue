<template>
  <main>
    <div class="form-control">
      <label class="label cursor-pointer">
        <span class="label-text">Enable debug mode logging</span>
        <input type="checkbox" class="toggle" checked />
      </label>
    </div>
    <div class="form-control">
      <label class="label cursor-pointer">
        <span class="label-text">Capture method</span>
        <select class="select w-1/4 max-w-xs">
          <option disabled selected>Pick screen capture method</option>
          <option v-for="(capture, key) in captureMethodList" :key="key">
            {{ capture }}
          </option>
        </select>
      </label>
    </div>
    <div class="form-control">
      <label class="label cursor-pointer">
        <span class="label-text">Capture compression mode</span>
        <select class="select w-1/4 max-w-xs">
          <option disabled selected>Pick compression mode</option>
          <option v-for="(compression, key) in compressionModeList" :key="key">
            {{ compression }}
          </option>
        </select>
      </label>
    </div>
  </main>
</template>

<script setup lang="ts">
import { useClassifierStore } from "@/stores/classifiers";
import { useConfigStore } from "@/stores/config";
import { mapActions } from "pinia";

const { captureMethodList, compressionModeList } = useClassifierStore();
</script>
