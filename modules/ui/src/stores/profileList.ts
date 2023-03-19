import type { AxiosStatic } from "axios";
import { inject, ref } from "vue";
import { defineStore } from "pinia";

export const useProfileListStore = defineStore("profileList", () => {
  const axios: AxiosStatic = <AxiosStatic>inject("axios");
  const $api = inject("$api");

  const profiles = ref<String[]>([]);

  const load = async () => {
    console.log("load start");
    const res = await axios.get($api + "/profiles");

    profiles.value = res.data;
  };

  const loadByName = async (name: string) => {
    await axios.get($api + "/profile/" + name);
  };

  const deleteByName = async (name: string) => {
    await axios.delete($api + "/profile/" + name);
  };

  return {
    profiles,
    load,
    loadByName,
    deleteByName,
  };
});
