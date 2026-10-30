
import axios from "axios";

const API = axios.create({
    baseURL: "http://localhost:8081/api/v1/documents",
    headers: {
        Accept: "application/json",
    },
});

export const uploadDocument = async (file) => {
    const formData = new FormData();
    formData.append("file", file);

    const response = await API.post("/upload", formData);
    return response.data;
};

export const getAllDocuments = async () => {
    const response = await API.get("");
    return response.data;
};

export const getDocumentById = async (id) => {
    const response = await API.get(`/${id}`);
    return response.data;
};

export const deleteDocument = async (id) => {
    const response = await API.delete(`/${id}`);
    return response.data;
};





