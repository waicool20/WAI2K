<template>
  <h2 class="text-xl my-4 px-1">Stop script settings</h2>
  <div class="form-control">
    <label class="label cursor-pointer">
      <span class="label-text">Enabled</span>
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
      <span class="label-text">Exit program</span>
      <input
        type="checkbox"
        class="toggle"
        v-model="exitProgram"
        :true-value="true"
        :false-value="false"
      />
    </label>
  </div>
  <h2 class="text-xl my-4 px-1">Count</h2>
  <div class="form-control">
    <label class="label cursor-pointer">
      <span class="label-text">Enabled</span>
      <input
        type="checkbox"
        class="toggle"
        v-model="countEnabled"
        :true-value="true"
        :false-value="false"
      />
    </label>
  </div>
  <div class="form-control">
    <label class="label cursor-pointer">
      <span class="label-text">Sorties</span>
      <input
        v-model="countSorties"
        type="number"
        step="1"
        min="1"
        max="1000"
        class="input w-1/4 max-w-xs"
      />
    </label>
  </div>
  <h2 class="text-xl my-4 px-1">Time</h2>
  <div class="form-control">
    <label class="label cursor-pointer">
      <span class="label-text">Enabled</span>
      <input
        type="checkbox"
        class="toggle"
        v-model="timeEnabled"
        :true-value="true"
        :false-value="false"
      />
    </label>
  </div>
  <div class="form-control">
    <label class="label cursor-pointer">
      <span class="label-text">Type</span>
      <span class="btn-group">
        <input
          v-for="(type, index) in timeStopType"
          type="radio"
          name="draggerSlot"
          :data-title="type"
          class="btn bg-neutral-focus"
          v-model="timeMode"
          :key="index"
          :value="type"
        />
      </span>
    </label>
  </div>
  <div class="form-control" v-if="timeMode === 'ELAPSED_TIME'">
    <label class="label cursor-pointer">
      <span class="label-text">Elapsed time</span>
      <span class="input-group max-w-lg">
        <input
          type="number"
          placeholder="Days"
          class="input w-1/4 max-w-xs"
          step="1"
          min="0"
          max="1000"
          :value="elapsedTime.days"
          @input="
            elapsedTime = updateInterval(
              elapsedTime,
              'days',
              $event
            )
          "
        />
        <input
          type="number"
          placeholder="Hours"
          class="input w-1/4 max-w-xs"
          step="1"
          min="0"
          max="1000"
          :value="elapsedTime.hours"
          @input="
            elapsedTime = updateInterval(
              elapsedTime,
              'hours',
              $event
            )
          "
        />
        <input
          type="number"
          placeholder="Minutes"
          class="input w-1/4 max-w-xs"
          step="1"
          min="0"
          max="1000"
          :value="elapsedTime.minutes"
          @input="
            elapsedTime = updateInterval(
              elapsedTime,
              'minutes',
              $event
            )
          "
        />
        <input
          type="number"
          placeholder="Seconds"
          class="input w-1/4 max-w-xs"
          step="1"
          min="0"
          max="1000"
          :value="elapsedTime.seconds"
          @input="
            elapsedTime = updateInterval(
              elapsedTime,
              'seconds',
              $event
            )
          "
        />
      </span>
    </label>
  </div>
  <div class="form-control" v-if="timeMode === 'SPECIFIC_TIME'">
    <label class="label cursor-pointer">
      <span class="label-text">Specific time</span>
      <span class="input-group max-w-md">
        <input
          type="number"
          placeholder="Hours"
          class="input w-1/2 max-w-xs"
          step="1"
          min="0"
          max="24"
          :value="specificTime.hours"
          @input="
            specificTime = updateInterval(
              specificTime,
              'hours',
              $event
            )
          "
        />
        <input
          type="number"
          placeholder="Minutes"
          class="input w-1/2 max-w-xs"
          step="1"
          min="0"
          max="59"
          :value="specificTime.minutes"
          @input="
            specificTime = updateInterval(
              specificTime,
              'minutes',
              $event
            )
          "
        />
      </span>
    </label>
  </div>
</template>
<script setup lang="ts">
import { Duration } from "luxon";
import { useProfileStore } from "@/stores/profile";
import { useClassifierStore } from "@/stores/classifiers";
import { computed } from "vue";

interface InputEvent extends Event {
  target: HTMLInputElement;
}

const store = useProfileStore();
const { timeStopType } = useClassifierStore();

const updateInterval = (
  object: Duration,
  property: string,
  event: Event
): Duration => object.set({ [property]: (event as InputEvent).target.value });

const enabled = computed({
  get() {
    return store.stopEnabled;
  },
  set(value) {
    store.setStopEnabled(value);
  },
});
const exitProgram = computed({
  get() {
    return store.stopExitProgram;
  },
  set(value) {
    store.setStopExitProgram(value);
  },
});
const countEnabled = computed({
  get() {
    return store.stopCountEnabled;
  },
  set(value) {
    store.setStopCountEnabled(value);
  },
});
const countSorties = computed({
  get() {
    return store.stopCountSorties;
  },
  set(value) {
    store.setStopCountSorties(value);
  },
});
const timeEnabled = computed({
  get() {
    return store.stopTimeEnabled;
  },
  set(value) {
    store.setStopTimeEnabled(value);
  },
});
const timeMode = computed({
  get() {
    return store.stopTimeMode;
  },
  set(value) {
    store.setStopTimeMode(value);
  },
});
const elapsedTime = computed({
  get() {
    return Duration.fromISO(store.stopTimeElapsedTime);
  },
  set(value) {
    store.setStopTimeElapsedTime(value.toISO());
  },
});
const specificTime = computed({
  get() {
    const specificTime = store.stopTimeSpecificTime.split(":");
    return Duration.fromObject({
      hours: parseInt(specificTime[0]),
      minutes: parseInt(specificTime[1]),
    });
  },
  set(value) {
    store.setStopTimeSpecificTime(value.toFormat("hh:mm"));
  },
});
</script>
