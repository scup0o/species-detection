package com.project.speciesdetection.core.helpers

import com.project.speciesdetection.core.services.remote_database.CloudinaryConstants
import javax.inject.Inject

object CloudinaryImageURLHelper{
    fun getBaseImageURL(image : String): String {
        return CloudinaryConstants.BASE_URL+CloudinaryConstants.VERSION+image
    }

    fun getSquareImageURL(image : String): String {
        return CloudinaryConstants.BASE_URL+CloudinaryConstants.TRANSFORMATION_SQUARE+CloudinaryConstants.VERSION+image
    }

}