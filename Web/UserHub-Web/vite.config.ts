/// <reference types="vitest/config" />

import { resolve } from "node:path"
import vue from "@vitejs/plugin-vue"
import vueJsx from "@vitejs/plugin-vue-jsx"
import UnoCSS from "unocss/vite"
import AutoImport from "unplugin-auto-import/vite"
import SvgComponent from "unplugin-svg-component/vite"
import { ElementPlusResolver } from "unplugin-vue-components/resolvers"
import Components from "unplugin-vue-components/vite"
import { defineConfig, loadEnv } from "vite"
import svgLoader from "vite-svg-loader"

// Configuring Vite: https://cn.vite.dev/config
export default defineConfig(({ mode }) => {
  const { VITE_PUBLIC_PATH } = loadEnv(mode, process.cwd(), "") as ImportMetaEnv
  return {
    // 寮€鍙戞垨鎵撳寘鏋勫缓鏃剁敤鍒扮殑鍏叡鍩虹璺緞
    base: VITE_PUBLIC_PATH,
    resolve: {
      alias: {
        // @ 绗﹀彿鎸囧悜 src 鐩綍
        "@": resolve(__dirname, "src"),
        // @@ 绗﹀彿鎸囧悜 src/common 閫氱敤鐩綍
        "@@": resolve(__dirname, "src/common")
      }
    },
    // 寮€鍙戠幆澧冩湇鍔″櫒閰嶇疆
    server: {
      // 鏄惁鐩戝惉鎵€鏈夊湴鍧€
      host: true,
      // 绔彛鍙?      port: 3333,
      // 绔彛琚崰鐢ㄦ椂锛屾槸鍚︾洿鎺ラ€€鍑?      strictPort: false,
      // 鏄惁鑷姩鎵撳紑娴忚鍣?      open: true,
      // 鍙嶅悜浠ｇ悊
      proxy: {
        "/api": {
          // target: "https://apifoxmock.com/m1/2930465-2145633-default",
          target: "http://127.0.0.1:8080",
          // 鏄惁涓?WebSocket
          ws: false,
          // 鏄惁鍏佽璺ㄥ煙
          changeOrigin: true
        }
      },
      // 鏄惁鍏佽璺ㄥ煙
      cors: true,
      // 棰勭儹甯哥敤鏂囦欢锛屾彁楂樺垵濮嬮〉闈㈠姞杞介€熷害
      warmup: {
        clientFiles: [
          "./src/layouts/**/*.*",
          "./src/pinia/**/*.*",
          "./src/router/**/*.*"
        ]
      }
    },
    // 鏋勫缓閰嶇疆
    build: {
      // 鑷畾涔夊簳灞傜殑 Rollup 鎵撳寘閰嶇疆
      rollupOptions: {
        output: {
          /**
           * @name 鍒嗗潡绛栫暐
           * @description 1. 娉ㄦ剰杩欎簺鍖呭悕蹇呴』瀛樺湪锛屽惁鍒欐墦鍖呬細鎶ラ敊
           * @description 2. 濡傛灉浣犱笉鎯宠嚜瀹氫箟 chunk 鍒嗗壊绛栫暐锛屽彲浠ョ洿鎺ョЩ闄よ繖娈甸厤缃?           */
          manualChunks: {
            vue: ["vue", "vue-router", "pinia"],
            element: ["element-plus", "@element-plus/icons-vue"],
            vxe: ["vxe-table"]
          }
        }
      },
      // 鏄惁寮€鍚?gzip 鍘嬬缉澶у皬鎶ュ憡锛岀鐢ㄦ椂鑳界暐寰彁楂樻瀯寤烘€ц兘
      reportCompressedSize: false,
      // 鍗曚釜 chunk 鏂囦欢鐨勫ぇ灏忚秴杩?2048kB 鏃跺彂鍑鸿鍛?      chunkSizeWarningLimit: 2048
    },
    // 娣锋穯鍣?    esbuild:
      mode === "development"
        ? undefined
        : {
            // 鎵撳寘鏋勫缓鏃剁Щ闄?console.log
            pure: ["console.log"],
            // 鎵撳寘鏋勫缓鏃剁Щ闄?debugger
            drop: ["debugger"],
            // 鎵撳寘鏋勫缓鏃剁Щ闄ゆ墍鏈夋敞閲?            legalComments: "none"
          },
    // 鎻掍欢閰嶇疆
    plugins: [
      vue(),
      // 鏀寔 JSX銆乀SX 璇硶
      vueJsx(),
      // 鏀寔灏?SVG 鏂囦欢瀵煎叆涓?Vue 缁勪欢
      svgLoader({
        defaultImport: "url",
        svgoConfig: {
          plugins: [
            {
              name: "preset-default",
              params: {
                overrides: {
                  // @see https://github.com/svg/svgo/issues/1128
                  removeViewBox: false
                }
              }
            }
          ]
        }
      }),
      // 鑷姩鐢熸垚 SvgIcon 缁勪欢鍜?SVG 闆ⅶ鍥?      SvgComponent({
        iconDir: [resolve(__dirname, "src/common/assets/icons")],
        preserveColor: resolve(__dirname, "src/common/assets/icons/preserve-color"),
        dts: true,
        dtsDir: resolve(__dirname, "types/auto")
      }),
      // 鍘熷瓙鍖?CSS
      UnoCSS(),
      // 鑷姩鎸夐渶瀵煎叆 API
      AutoImport({
        imports: ["vue", "vue-router", "pinia"],
        dts: "types/auto/auto-imports.d.ts",
        resolvers: [ElementPlusResolver()]
      }),
      // 鑷姩鎸夐渶瀵煎叆缁勪欢
      Components({
        dts: "types/auto/components.d.ts",
        resolvers: [ElementPlusResolver()]
      })
    ],
    // Configuring Vitest: https://cn.vitest.dev/config
    test: {
      include: ["tests/**/*.test.{ts,js}"],
      environment: "happy-dom",
      server: {
        deps: {
          inline: ["element-plus"]
        }
      }
    }
  }
})
