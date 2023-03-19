<template>
  <h2 class="text-xl my-4 px-1">Combat report</h2>
  <div class="form-control">
    <label class="label cursor-pointer">
      <span class="label-text">Enable Kalina overdrive</span>
      <input
        type="checkbox"
        class="toggle"
        v-model="enabled"
        :true-value="true"
        :false-value="false"
      />
    </label>
  </div>
  <div class="form-control">
    <label class="label cursor-pointer">
      <span class="label-text">Type</span>
      <select class="select w-1/4 max-w-xs" v-model="type">
        <option
          v-for="(combatReportType, index) in combatReportTypeList"
          :key="index"
        >
          {{ combatReportType }}
        </option>
      </select>
    </label>
  </div>
</template>
<script setup lang="ts">
import { useProfileStore } from "@/stores/profile";
import { computed } from "vue";
import {useClassifierStore} from "@/stores/classifiers";

const store = useProfileStore();
const { combatReportTypeList } = useClassifierStore();

const enabled = computed({
  get() {
    return store.combatReportEnabled;
  },
  set(value) {
    store.setCombatReportEnabled(value);
  },
});

const type = computed({
  get() {
    return store.combatReportType;
  },
  set(value) {
    store.setCombatReportType(value);
  },
});
</script>
