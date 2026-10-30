
import axios from "axios";

const CHAT_API = axios.create({
    baseURL: "http://localhost:8081/api/v1/chat",
    headers: {
        Accept: "application/json",
        "Content-Type": "application/json",
    },
});

export const askQuestion = async (payload) => {
    const response = await CHAT_API.post("", payload);
    return response.data;
};
