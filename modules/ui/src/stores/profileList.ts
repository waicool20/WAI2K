import type { AxiosStatic } from "axios";
import type { Ref } from "vue";
import { inject, ref } from "vue";
import { defineStore } from "pinia";
import { useProfileStore } from "@/stores/profile";

interface ProfilesStore {
  profiles: Ref<String[]>;
  currentProfile: Ref<String>;
  getAll: CallableFunction;
  loadByName: CallableFunction;
}

export const useProfileListStore = defineStore("profileList", () => {
  const axios: AxiosStatic = <AxiosStatic>inject("axios");
  const $api = inject("$api");

  const profiles = ref<String[]>([]);

  const load = async () => {
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
