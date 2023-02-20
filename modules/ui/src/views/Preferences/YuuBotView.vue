<template>
  <main>
    <h3 class="ml-1 my-4 uppercase">YuuBot</h3>
    <div class="form-control">
      <label class="label cursor-pointer">
        <span class="label-text">API key</span>
        <input
          type="text"
          class="input input-bordered w-1/2 max-w-md"
          v-model="apiKey"
          :class="[apiKeyClass]"
        />
      </label>
    </div>
    <h3 class="ml-1 my-4 uppercase">Notifications</h3>
    <div class="form-control">
      <label class="label cursor-pointer">
        <span class="label-text">On restart</span>
        <input
          type="checkbox"
          class="toggle"
          v-model="notificationOnRestart"
          :true-value="true"
          :false-value="false"
        />
      </label>
    </div>
    <div class="form-control">
      <label class="label cursor-pointer">
        <span class="label-text">On stop condition</span>
        <input
          type="checkbox"
          class="toggle"
          v-model="notificationOnStopCondition"
          :true-value="true"
          :false-value="false"
        />
      </label>
    </div>
  </main>
  <h3 class="ml-1 my-4 uppercase">Send test message</h3>
  <form ref="yuu-test">
    <fieldset>
      <legend class="ml-1 w-full">
        Test message
        <button
          @click.prevent="() => sendTestMessage()"
          class="btn btn-primary btn-xs float-right"
        >
          Send
        </button>
      </legend>

      <div class="form-control my-4">
        <input
          type="text"
          class="input w-full"
          placeholder="Message title"
          ref="title"
        />
      </div>
      <div class="form-control">
        <textarea
          class="textarea h-24"
          placeholder="Message body"
          ref="message"
        ></textarea>
      </div>
    </fieldset>
  </form>
</template>
<script setup lang="ts">
import { useConfigStore } from "@/stores/config";
import {computed, onMounted, reactive, ref} from "vue";

const store = useConfigStore();

const title = ref(null);
const message = ref(null);

const apiKeyStatus = ref("");

const apiKeyClass = computed({
  get() {
    return apiKeyStatus.value;
  },
  set(value) {
    apiKeyStatus.value = "input-" + value;
  },
});

const apiKey = computed({
  get() {
    return store.apiKey;
  },
  async set(value) {
    store.setApiKey(value);
    store.checkApiKey().then((status) => {
      apiKeyClass.value = status;
    });
  },
});

onMounted(() => {
  store.checkApiKey().then((status) => {
    apiKeyClass.value = status;
  });
});

const notificationOnRestart = computed({
  get() {
    return store.notificationOnRestart;
  },
  set(value) {
    store.setNotificationOnRestart(value);
  },
});
const notificationOnStopCondition = computed({
  get() {
    return store.notificationOnStopCondition;
  },
  set(value) {
    store.setNotificationOnStopCondition(value);
  },
});

const sendTestMessage = () => {
  // @ts-ignore
  store.sendTestMessage(title.value.value, message.value.value);
};
</script>
