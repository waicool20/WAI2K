import type {StoreDefinition} from "pinia";

export {}

declare module 'vue-router' {
    interface RouteMeta {
        store?: StoreDefinition
        title: string
    }
}