import { createHtmlPlugin } from 'vite-plugin-html'

export default function createHtml(env, isBuild) {
    const { VITE_APP_TITLE, VITE_APP_DEBUG_TOOL } = env
    const html = createHtmlPlugin({
        inject: {
            data: {
                title: VITE_APP_TITLE,
                debugTool: VITE_APP_DEBUG_TOOL,
                copyrightScript: ''
            }
        },
        minify: isBuild
    })
    return html
}
