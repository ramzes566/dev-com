export interface User {
    id: string
    username: string
    nickname: string | null
}

export async function listUsers(): Promise<User[]> {
    const res = await fetch('/api/users')
    if(!res.ok) throw new Error(`HTTP ${res.status}`)
    return res.json()
}