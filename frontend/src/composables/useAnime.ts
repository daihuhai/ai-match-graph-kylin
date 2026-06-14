import { onMounted, onUnmounted, type Ref } from 'vue'
import { animate, createTimeline, stagger } from 'animejs'

export interface UseAnimeOptions {
  /** 动画目标 (CSS 选择器或 DOM 元素) */
  targets: string | Element | Element[] | NodeList | Node[]
  /** anime.js 动画参数 */
  params: Parameters<typeof animate>[1]
  /** 是否挂载时自动播放，默认 true */
  autoplay?: boolean
}

export function useAnime(options: UseAnimeOptions) {
  const { targets, params, autoplay = true } = options
  let instance: ReturnType<typeof animate> | null = null

  const init = () => {
    instance = animate(targets, { ...params, autoplay: false })
    if (autoplay) instance.play()
  }

  const play = () => instance?.play()
  const pause = () => instance?.pause()
  const restart = () => instance?.restart()
  const reverse = () => instance?.reverse()
  const seek = (time: number) => instance?.seek(time)

  onMounted(init)
  onUnmounted(() => instance?.pause())

  return { instance, play, pause, restart, reverse, seek }
}

/** 入场动画：从下方淡入上滑 */
export function useFadeInUp(selector: string, delay = 0, duration = 600) {
  return useAnime({
    targets: selector,
    params: {
      translateY: [30, 0],
      opacity: [0, 1],
      easing: 'easeOutCubic',
      duration,
      delay,
    },
  })
}

/** 入场动画：缩放淡入 */
export function useScaleIn(selector: string, delay = 0, duration = 500) {
  return useAnime({
    targets: selector,
    params: {
      scale: [0.95, 1],
      opacity: [0, 1],
      easing: 'easeOutCubic',
      duration,
      delay,
    },
  })
}

/** 数字滚动动画 */
export function useCountUp(elRef: Ref<Element | undefined>, targetValue: number, duration = 1000) {
  const obj = { value: 0 }
  return useAnime({
    targets: obj as unknown as string | Element | Element[] | NodeList | Node[],
    params: {
      value: targetValue,
      round: 1,
      easing: 'easeOutExpo',
      duration,
      update: () => {
        if (elRef.value) elRef.value.textContent = String(obj.value)
      },
    },
  })
}
